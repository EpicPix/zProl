const fs = require('fs');
const process = require('process');
const { spawn } = require('child_process');

const outputs = JSON.parse(fs.readFileSync('.outputs.json').toString());

async function main() {
    var failed = false;
    for(const file of fs.readdirSync('.')) {
        if(file.endsWith('.zprol')) {
            const process = spawn('java', ['-jar', '../zProL.jar', file]);
            const finish = await new Promise((resolve) => process.on('close', resolve));
            const f = file.substring(0, file.length - ".zprol".length);
            const out = outputs[f];
            if(out.exit != finish) {
                failed = true;
                console.error(`Test failed for ${file}`);
            }
        }
    }
    if(failed) {
        process.exit(1);
    }
}

main();
