const fs = require("fs");
function edit(p, pairs) {
  const raw = fs.readFileSync(p, "utf8");
  const crlf = raw.includes("\r\n");
  let s = raw.replace(/\r\n/g, "\n");
  for (const [a, b] of pairs) {
    if (!s.includes(a)) throw new Error(p + " anchor: " + a.slice(0, 50));
    s = s.replace(a, b);
  }
  fs.writeFileSync(p, crlf ? s.replace(/\n/g, "\r\n") : s);
}
const readme = fs.readFileSync("README.md", "utf8");
const old = readme.match(/\*\*Status:\*\*[^\n]*\n/)[0];
const status = `**Status:** 17.7 (versionCode 179) is built from commit \`52faa21b\`. The signed bundle \`quran-17.7-vc179.aab\` has SHA-256 \`056e563c134bd2b01dd40d53093fe4b3e3301f1709ac75f968f4e896134b37e5\`, and \`jarsigner\` reads X.509 \`CN=Count & Play Admin, OU=Engineering, O=Count & Play, C=US\` with SHA-256 \`53:7D:09:D2:03:00:12:9E:97:3B:79:45:31:6B:FE:24:CF:AD:CF:BC:77:EE:C5:22:9C:BF:30:17:0D:9D:E5:21\`, the upload key 17.6 was signed with. It was built on this machine and not from a hosted \`build\` run: run 42981599114 lost the phone emulator on both attempts in all three shards on \`device 'emulator-5554' not found\` with no assertion failure, the failure the 17.6 record already carries, so its signed-bundle job was skipped. The local evidence is the JVM suite, lint and \`assembleDebug\`, the data suite 56 of 56, and the app suite 213 of 217 on the tablet AVD, whose four reds are accounted for: one read the Save row's spoken state that this release moved into the row's word, it is fixed and green since, and the other three are that AVD's own, green on the phone AVD in isolation. The phone leg of capture run 42981599083 failed on the same emulator loss, so the store's phone set is still 17.6's and the two tablet sets are this run's. Content 1.7.0, database \`content-db-2605fb00\`, and the \`pack-word-root-97fd42f4\` pack are published. The Quran text, both translations, both tafsirs, and the audio are unchanged.\n`;
edit("README.md", [[old, status]]);
edit("play-store/listing.md", [
  ["The set is from capture run 42826202144 on 17.5: all three legs green,", "The two tablet sets are from capture run 42981599083 on 17.7, green on both\nlegs; the phone set is still the 17.6 run's, because this run's phone leg\nlost its emulator. The 17.5 run below was the last complete set: all three\nlegs green,"],
]);
console.log("record written");
