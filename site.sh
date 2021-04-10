#!/bin/bash -e

pf target/mdoc target/site
pushd target/site
tar czvf ../herding-cats.tar.gz *
popd
