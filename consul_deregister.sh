#!/bin/sh

curl -s http://localhost:8500/v1/health/service/siglusapi | jq -r '.[] | "curl -XPUT http://localhost:8500/v1/agent/service/deregister/" + .Service.ID' >> clear.sh
chmod a+x clear.sh && ./clear.sh
