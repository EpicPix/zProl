const fs = require('fs');
const process = require('process');
const { spawn } = require('child_process');

const outputs = JSON.parse(fs.readFileSync('.outputs.json').toString());

async function main() {
    var fails = [];
    for(const file of fs.readdirSync('.')) {
        if(file.endsWith('.zprol')) {
            var nfile = file.substring(0, file.length - ".zprol".length);
            console.log(`----------${nfile}----------`);
            console.log(file);
            const process = spawn('java', ['-jar', '../zProL.jar', file], {stdio: ['ignore', 'pipe', 'pipe']});
            process.stdout.setEncoding('utf8');
            var data = "";
            process.stdout.on('data', (chunk) => data += chunk).on("error", () => {});
            process.stderr.on('data', (chunk) => data += chunk).on("error", () => {});
            const finish = await new Promise((resolve) => process.on('close', resolve));
            const out = outputs[nfile];
            if(out.exit != finish) {
                console.log(data);
                console.log(`Test failed`);
                fails.push(file);
            }else {
                console.log(`Test succeeded`);
            }
        }
    }
    if(fails.length) {
        console.log(fails.map((file) => `Test failed for ${file}`).join("\n"));
        process.exit(1);
    }
}

main();
