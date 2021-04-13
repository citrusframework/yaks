module github.com/citrusframework/yaks

go 1.13

require (
	github.com/Masterminds/semver v1.5.0
	github.com/container-tools/snap v0.0.8
	github.com/gertd/go-pluralize v0.1.7
	github.com/go-logr/logr v0.4.0
	github.com/google/uuid v1.2.0
	github.com/mitchellh/go-homedir v1.1.0
	github.com/mitchellh/mapstructure v1.1.2
	github.com/openshift/api v3.9.1-0.20190927182313-d4a64ec2cbd8+incompatible
	github.com/operator-framework/api v0.3.8
	github.com/operator-framework/operator-lib v0.1.0
	github.com/pkg/errors v0.9.1
	github.com/prometheus-operator/prometheus-operator/pkg/apis/monitoring v0.42.1
	github.com/rs/xid v1.2.1
	github.com/shurcooL/httpfs v0.0.0-20190707220628-8d4bc4ba7749
	github.com/shurcooL/vfsgen v0.0.0-20181202132449-6a9ea43bcacd
	github.com/sirupsen/logrus v1.7.0
	github.com/spf13/cobra v1.1.1
	github.com/spf13/pflag v1.0.5
	github.com/spf13/viper v1.7.0
	github.com/stoewer/go-strcase v1.2.0
	github.com/stretchr/testify v1.6.1
	gopkg.in/yaml.v2 v2.4.0
	k8s.io/api v0.19.8
	k8s.io/apiextensions-apiserver v0.19.8
	k8s.io/apimachinery v0.19.8
	k8s.io/client-go v12.0.0+incompatible
	k8s.io/klog/v2 v2.5.0
	knative.dev/eventing v0.21.1
	sigs.k8s.io/controller-runtime v0.7.2
)

replace (
	k8s.io/api => k8s.io/api v0.19.8
	k8s.io/apimachinery => k8s.io/apimachinery v0.19.8
	k8s.io/client-go => k8s.io/client-go v0.19.8
)

replace github.com/docker/docker => github.com/moby/moby v0.7.3-0.20190826074503-38ab9da00309 // Required by Helm
