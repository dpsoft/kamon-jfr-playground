import http from "k6/http";
import { check, group } from "k6";

export let options = {
    vus: 1,
    stages: [
        { duration: "1m", target: 3 },
        { duration: "3m", target: 5 },
        { duration: "5m", target: 10 },
        { duration: "1m30s", target: 0 },
    ]
};

export default function() {
    group("GET", function() {
        let res = http.get("http://localhost:8080");
        check(res, {"status is 200": (r) => r.status === 200});
    });
};