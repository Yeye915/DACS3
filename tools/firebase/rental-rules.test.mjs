import { before, beforeEach, after, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, collection, getDoc, getDocs, query, where, setDoc, updateDoc, deleteDoc, runTransaction, serverTimestamp } from 'firebase/firestore';
import { ref, uploadBytes } from 'firebase/storage';

let env;
const db = (uid) => uid ? env.authenticatedContext(uid, {email:`${uid}@example.test`}).firestore() : env.unauthenticatedContext().firestore();
const room = {hostId:'host',hostUid:'host',tenantUid:'',title:'Phòng có cửa sổ',location:'Hải Châu, Đà Nẵng',rental:'Hải Châu, Đà Nẵng',price:'2.500.000 đ/tháng',monthlyPrice:2500000,area:'25',bedroom:1,bathroom:1,imageUrl:'',status:'Trống',latitude:16.047,longitude:108.206,rating:0};
const requestData = (tenant='tenant') => ({propertyId:'room',roomName:room.title,hostId:'host',tenantUid:tenant,tenantName:`Người thuê ${tenant}`,status:'pending',createdAt:serverTimestamp()});
const requestId = (tenant='tenant') => `room~${tenant}`;
async function request(tenant='tenant') { await setDoc(doc(db(tenant),'rental_requests',requestId(tenant)),requestData(tenant)); }
async function approve(tenant='tenant') {
  const firestore=db('host');
  await runTransaction(firestore,async tx => {
    const rr=doc(firestore,'rental_requests',requestId(tenant)); const pr=doc(firestore,'properties','room');
    const r=await tx.get(rr); const p=await tx.get(pr);
    assert.equal(r.data().status,'pending');assert.equal(p.data().tenantUid,'');
    tx.update(pr,{tenantUid:tenant,status:'Đã thuê',rentalRequestId:requestId(tenant)});
    tx.set(doc(firestore,'handovers',requestId(tenant)),{propertyId:'room',requestId:requestId(tenant),roomName:room.title,hostId:'host',tenantUid:tenant,tenantName:tenant,assetCheck:'Kiểm tra thực tế',completed:false,createdAt:serverTimestamp()});
    tx.update(rr,{status:'accepted',updatedAt:serverTimestamp()});
  });
}
const bill = {propertyId:'room',roomName:room.title,hostId:'host',tenantUid:'tenant',month:'06/2026',monthKey:'2026-06',roomFee:2500000,electricityFee:100000,waterFee:50000,totalAmount:2650000,status:'Chưa thanh toán',createdAt:serverTimestamp()};
const billId='room~tenant~2026-06';

before(async () => {
  assert.equal(process.env.GCLOUD_PROJECT || 'demo-house','demo-house','Tests must use the demo project');
  env=await initializeTestEnvironment({projectId:'demo-house',firestore:{host:'127.0.0.1',port:8080,rules:await readFile(new URL('../../firestore.rules',import.meta.url),'utf8')},storage:{host:'127.0.0.1',port:9199,rules:await readFile(new URL('../../storage.rules',import.meta.url),'utf8')}});
});
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async context => {
    const f=context.firestore();
    await setDoc(doc(f,'Users','host'),{fullName:'Chủ nhà',email:'host@example.test',phone:'0901234567',role:'host'});
    await setDoc(doc(f,'properties','room'),room);
  });
});
after(async () => { await env?.cleanup(); });

test('Guests can browse; only owners edit, and nobody can directly claim a room',async () => {
  await assertSucceeds(getDoc(doc(db(),'properties','room')));
  await assertFails(setDoc(doc(db(),'properties','another'),room));
  await assertFails(updateDoc(doc(db('tenant'),'properties','room'),{tenantUid:'tenant',status:'Đã thuê'}));
  await assertFails(updateDoc(doc(db('host'),'properties','room'),{tenantUid:'tenant',status:'Đã thuê'}));
  await assertSucceeds(updateDoc(doc(db('host'),'properties','room'),{title:'Phòng đã chỉnh sửa'}));
  await assertFails(updateDoc(doc(db('other'),'properties','room'),{title:'Tin giả'}));
});
test('Favorites persist per UID and cannot be read or changed by another account',async () => {
  const own=doc(db('tenant'),'Users','tenant','favorites','room');
  await assertSucceeds(setDoc(own,{propertyId:'room',savedAt:serverTimestamp()}));
  assert.equal((await getDoc(own)).exists(),true);
  await assertFails(getDoc(doc(db('other'),'Users','tenant','favorites','room')));
  await assertFails(deleteDoc(doc(db('other'),'Users','tenant','favorites','room')));
  await assertSucceeds(deleteDoc(own));
});
test('Rental requests enforce sender identity, one request ID, ownership and private queries',async () => {
  await assertSucceeds(request());
  await assertFails(setDoc(doc(db('other'),'rental_requests','room~other'),requestData('tenant')));
  await assertFails(setDoc(doc(db('tenant'),'rental_requests','duplicate'),requestData()));
  await assertFails(getDoc(doc(db('other'),'rental_requests',requestId())));
  await assertFails(getDocs(collection(db('tenant'),'rental_requests')));
  assert.equal((await getDocs(query(collection(db('tenant'),'rental_requests'),where('tenantUid','==','tenant')))).size,1);
  await assertSucceeds(updateDoc(doc(db('tenant'),'rental_requests',requestId()),{status:'cancelled'}));
  await assertSucceeds(request());
});
test('Approval atomically links room, request and handover; competing requests cannot take it',async () => {
  await request();await request('other');
  await assertFails(updateDoc(doc(db('host'),'rental_requests',requestId()),{status:'accepted'}));
  await assertSucceeds(approve());
  assert.equal((await getDoc(doc(db(),'properties','room'))).data().tenantUid,'tenant');
  await assert.rejects(approve('other'));
  await assertFails(request('late'));
  assert.equal((await getDoc(doc(db('host'),'rental_requests',requestId('other')))).data().status,'pending');
});
test('Only the designated tenant can sign a handover; signing cannot be undone',async () => {
  await request();await approve();
  const sign={completed:true,signedAt:serverTimestamp()};
  await assertFails(updateDoc(doc(db('host'),'handovers',requestId()),sign));
  await assertFails(updateDoc(doc(db('other'),'handovers',requestId()),sign));
  await assertSucceeds(updateDoc(doc(db('tenant'),'handovers',requestId()),sign));
  await assertFails(updateDoc(doc(db('tenant'),'handovers',requestId()),{completed:false}));
});
test('Bills validate totals, tenant, owner and deterministic month ID; duplicate issue fails',async () => {
  await request();await approve();
  const own=doc(db('host'),'bills',billId);
  await assertFails(setDoc(own,{...bill,totalAmount:1}));
  await assertFails(setDoc(own,{...bill,roomFee:9000000000000000000,totalAmount:9000000000000150000}));
  await assertFails(setDoc(own,{...bill,tenantUid:'other'}));
  await assertFails(setDoc(doc(db('other'),'bills',billId),bill));
  await assertFails(setDoc(doc(db('host'),'bills','duplicate'),bill));
  await assertSucceeds(setDoc(own,bill));await assertFails(setDoc(own,bill));
  await assertFails(getDoc(doc(db('other'),'bills',billId)));
});
test('Tenant reports payment; owner confirms receipt; neither can alter charges or skip steps',async () => {
  await request();await approve();await setDoc(doc(db('host'),'bills',billId),bill);
  await assertFails(updateDoc(doc(db('tenant'),'bills',billId),{status:'Đã thanh toán'}));
  await assertSucceeds(updateDoc(doc(db('tenant'),'bills',billId),{status:'Chờ xác nhận'}));
  await assertFails(updateDoc(doc(db('host'),'bills',billId),{status:'Đã thanh toán',totalAmount:0}));
  await assertSucceeds(updateDoc(doc(db('host'),'bills',billId),{status:'Đã thanh toán'}));
});
test('Conversations and messages are restricted to participants and authenticated sender',async () => {
  const f=db('tenant');const c=doc(f,'conversations','thread');
  await assertSucceeds(setDoc(c,{participants:['host','tenant'],propertyId:'room',roomName:room.title,createdBy:'tenant',lastText:'',lastTime:0}));
  const message={senderId:'tenant',senderName:'Người thuê',text:'Tôi muốn xem phòng',time:1};
  await assertSucceeds(setDoc(doc(f,'conversations','thread','messages','one'),message));
  assert.equal((await getDoc(doc(f,'conversations','thread','messages','one'))).data().text,message.text);
  await assertFails(getDoc(doc(db('other'),'conversations','thread')));
  await assertFails(getDocs(collection(db('other'),'conversations','thread','messages')));
  await assertFails(setDoc(doc(db('host'),'conversations','thread','messages','two'),message));
  await assertFails(updateDoc(doc(db('host'),'conversations','thread'),{participants:['host','other']}));
});
test('Comments require authentication and an existing property ID',async () => {
  const comment={propertyId:'room',userUid:'tenant',username:'Khách',content:'Phòng còn không?',createdAt:serverTimestamp()};
  await assertFails(setDoc(doc(db(),'comments','guest'),comment));
  await assertFails(setDoc(doc(db('other'),'comments','spoof'),comment));
  await assertSucceeds(setDoc(doc(db('tenant'),'comments','comment'),comment));
});
test('Assets and repairs use property ownership and the actual tenant',async () => {
  await request();await approve();
  const asset={propertyId:'room',hostId:'host',roomName:room.title,assetName:'Máy lạnh',quantity:1,condition:'Tốt'};
  await assertSucceeds(setDoc(doc(db('host'),'assets','aircon'),asset));
  await assertSucceeds(getDocs(query(collection(db('tenant'),'assets'),where('propertyId','==','room'))));
  await assertFails(getDoc(doc(db('other'),'assets','aircon')));
  await assertFails(setDoc(doc(db('other'),'assets','bad'),asset));
  const repair={propertyId:'room',propertyTitle:room.title,hostId:'host',tenantUid:'tenant',description:'Máy lạnh không chạy',status:'Chờ xử lý',timestamp:1};
  await assertFails(setDoc(doc(db('other'),'repairs','bad'),repair));
  await assertSucceeds(setDoc(doc(db('tenant'),'repairs','repair'),repair));
  await assertSucceeds(updateDoc(doc(db('host'),'repairs','repair'),{status:'Đang sửa'}));
  await assertFails(updateDoc(doc(db('tenant'),'repairs','repair'),{status:'Đã xong'}));
});
test('Profiles never accept plaintext passwords or client role changes',async () => {
  const f=db('tenant');const p=doc(f,'Users','tenant');
  const profile={fullName:'Người thuê',email:'tenant@example.test',phone:'0901234567',role:'seeker'};
  await assertFails(setDoc(p,{...profile,password:'must-not-store'}));
  await assertSucceeds(setDoc(p,profile));
  await assertFails(updateDoc(p,{role:'host'}));
  await assertFails(updateDoc(p,{password:'must-not-store'}));
  await assertFails(getDoc(doc(db('other'),'Users','tenant')));
});
test('Image uploads enforce UID path and JPEG type',async () => {
  const storage=env.authenticatedContext('tenant').storage();
  await assertSucceeds(uploadBytes(ref(storage,'users/tenant/uploads/own.jpg'),new Uint8Array([255,216,255]),{contentType:'image/jpeg'}));
  await assertFails(uploadBytes(ref(storage,'users/other/uploads/spoof.jpg'),new Uint8Array([255]),{contentType:'image/jpeg'}));
  await assertFails(uploadBytes(ref(storage,'users/tenant/uploads/bad.txt'),new Uint8Array([65]),{contentType:'text/plain'}));
});
