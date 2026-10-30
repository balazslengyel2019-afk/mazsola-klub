// Tests the actual timing functions extracted from the Android APK's prepared game.js.
const fs=require('node:fs');
const vm=require('node:vm');
const assert=require('node:assert/strict');
const js=fs.readFileSync(process.argv[2],'utf8');
function between(start,end) {
 const a=js.indexOf(start);
 assert.ok(a>=0,'Missing source marker: '+start);
 const b=js.indexOf(end,a+start.length);
 assert.ok(b>a,'Missing source marker: '+end);
 return js.slice(a,b);
}
const functions=between('function difficulty(){','function occupied(')+
                between('function levelProgress(){','function spawn(){');
const c=vm.createContext({hits:0,level:1,rand:(lo,hi)=>(lo+hi)/2,Math});
vm.runInContext(functions,c);
for(const [hits,expect] of [[0,1],[7,1],[8,2],[40,6],[144,19],[152,20],[900,20]]){
 c.hits=hits;assert.equal(c.difficulty(),expect,'Level for '+hits+' hits');
}
let prevSpawn=Infinity,prevShot=Infinity,prevImmunity=Infinity;
for(let lvl=1;lvl<=20;lvl++){
 c.level=lvl;
 const spawn=c.spawnInterval(),flight=c.enemyFlightDuration(),immunity=c.damageImmunity();
 assert.ok(spawn<=prevSpawn+1e-7,'Spawn frequency must intensify');
 assert.ok(flight<=prevShot+1e-7,'Snowballs must accelerate');
 assert.ok(immunity<=prevImmunity+1e-7,'Immunity must shorten');
 prevSpawn=spawn;prevShot=flight;prevImmunity=immunity;
 for(const initial of [true,false]){
  const hp1=c.attackDelay({maxHp:1},initial);
  const hp2=c.attackDelay({maxHp:2},initial);
  const boss=c.attackDelay({maxHp:3},initial);
  assert.ok(Math.abs((hp1-hp2)-.5)<1e-9,'HP2 must attack exactly 0.5s faster at level '+lvl);
  assert.ok(Math.abs((hp1-boss)-1)<1e-9,'Boss must attack exactly 1s faster at level '+lvl);
 }
}
c.level=1;assert.equal(c.activeLimit(),2);assert.equal(c.enemyShotLimit(),2);
c.level=20;assert.equal(c.activeLimit(),7);assert.equal(c.enemyShotLimit(),6);
assert.ok(c.spawnInterval()<1.55);
assert.ok(c.enemyFlightDuration()<.6);
assert.ok(c.damageImmunity()<1.5);
for(const s of ["function showChampionCelebration()", "MEGÜBERNAGYON JÓ", "championCelebrating=4.5", "life+=restored", "showChampionCelebration();"]) assert.ok(js.includes(s),s);
console.log('Android v1.5 gameplay tests passed: 20 stages, HP timing, intensity and celebration.');
