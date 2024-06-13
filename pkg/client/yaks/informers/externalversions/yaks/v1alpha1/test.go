/*
 * Copyright the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Code generated by informer-gen. DO NOT EDIT.

package v1alpha1

import (
	"context"
	time "time"

	yaksv1alpha1 "github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	versioned "github.com/citrusframework/yaks/pkg/client/yaks/clientset/versioned"
	internalinterfaces "github.com/citrusframework/yaks/pkg/client/yaks/informers/externalversions/internalinterfaces"
	v1alpha1 "github.com/citrusframework/yaks/pkg/client/yaks/listers/yaks/v1alpha1"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	runtime "k8s.io/apimachinery/pkg/runtime"
	watch "k8s.io/apimachinery/pkg/watch"
	cache "k8s.io/client-go/tools/cache"
)

// TestInformer provides access to a shared informer and lister for
// Tests.
type TestInformer interface {
	Informer() cache.SharedIndexInformer
	Lister() v1alpha1.TestLister
}

type testInformer struct {
	factory          internalinterfaces.SharedInformerFactory
	tweakListOptions internalinterfaces.TweakListOptionsFunc
	namespace        string
}

// NewTestInformer constructs a new informer for Test type.
// Always prefer using an informer factory to get a shared informer instead of getting an independent
// one. This reduces memory footprint and number of connections to the server.
func NewTestInformer(client versioned.Interface, namespace string, resyncPeriod time.Duration, indexers cache.Indexers) cache.SharedIndexInformer {
	return NewFilteredTestInformer(client, namespace, resyncPeriod, indexers, nil)
}

// NewFilteredTestInformer constructs a new informer for Test type.
// Always prefer using an informer factory to get a shared informer instead of getting an independent
// one. This reduces memory footprint and number of connections to the server.
func NewFilteredTestInformer(client versioned.Interface, namespace string, resyncPeriod time.Duration, indexers cache.Indexers, tweakListOptions internalinterfaces.TweakListOptionsFunc) cache.SharedIndexInformer {
	return cache.NewSharedIndexInformer(
		&cache.ListWatch{
			ListFunc: func(options v1.ListOptions) (runtime.Object, error) {
				if tweakListOptions != nil {
					tweakListOptions(&options)
				}
				return client.YaksV1alpha1().Tests(namespace).List(context.TODO(), options)
			},
			WatchFunc: func(options v1.ListOptions) (watch.Interface, error) {
				if tweakListOptions != nil {
					tweakListOptions(&options)
				}
				return client.YaksV1alpha1().Tests(namespace).Watch(context.TODO(), options)
			},
		},
		&yaksv1alpha1.Test{},
		resyncPeriod,
		indexers,
	)
}

func (f *testInformer) defaultInformer(client versioned.Interface, resyncPeriod time.Duration) cache.SharedIndexInformer {
	return NewFilteredTestInformer(client, f.namespace, resyncPeriod, cache.Indexers{cache.NamespaceIndex: cache.MetaNamespaceIndexFunc}, f.tweakListOptions)
}

func (f *testInformer) Informer() cache.SharedIndexInformer {
	return f.factory.InformerFor(&yaksv1alpha1.Test{}, f.defaultInformer)
}

func (f *testInformer) Lister() v1alpha1.TestLister {
	return v1alpha1.NewTestLister(f.Informer().GetIndexer())
}
