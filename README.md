# Yaks

**Proof of Concept**

YAKS (Yeat Another Kamel Subproject)

## Usage

```
# Build binaries and image
eval $(minishift docker-env)
make && make images-dev

# Once per cluster
oc login -u system:admin
./yaks-cli install --cluster-setup

# Once per namespace
oc login -u developer
oc new-project yaks
./yaks-cli install

# Run the test
# expects a camel k integration named simple to be running and printing "Hello Camel"
./yaks-cli test examples/simple.feature

# Check the status of all tests
oc get tests
```
