import http from 'k6/http';
import { check } from 'k6';
import { SharedArray } from 'k6/data';

const HOT_COUNT = 50;
const COLD_COUNT = 950;
const HOT_REPETITIONS = 171;

const weightedCodes = new SharedArray('weighted-codes', function() {
    const codes = [];

    for (let rep = 0; rep < HOT_REPETITIONS; rep++) {
        for (let i = 1; i <= HOT_COUNT; i++) {
            codes.push(`hot-${String(i).padStart(3, '0')}`);
        }
    }

    for (let i = 1; i <= COLD_COUNT; i++) {
        codes.push(`cold-${String(i).padStart(3, '0')}`);
    }

    return codes;
});

export const options = {
    scenarios: {
        warmup: {
            executor: 'constant-vus',
            vus: 50,
            duration: '20s',
            exec: 'warmupRequest',
        },
        baseline: {
            executor: 'constant-vus',
            vus: 50,
            duration: '30s',
            startTime: '20s',
            exec: 'baselineRequest',
        },
    },
    thresholds: {
        'http_req_duration{scenario:baseline}': [],
    },
};

function executeRedirectRequest() {
    const randomCode = weightedCodes[Math.floor(Math.random() * weightedCodes.length)];

    const res = http.get(`http://localhost:8080/${randomCode}`, {
        redirects: 0
    });

    check(res, {
        'is status 302': (r) => r.status === 302,
    });
}

export function warmupRequest() {
    executeRedirectRequest();
}

export function baselineRequest() {
    executeRedirectRequest();
}