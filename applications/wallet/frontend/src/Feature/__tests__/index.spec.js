import TestRenderer, { act } from 'react-test-renderer'

import Feature, { ENVS } from '..'

describe('<Feature />', () => {
  it('should not render a disabled feature', () => {
    localStorage.setItem('FeatureFlags', JSON.stringify({ hello: false }))

    const component = TestRenderer.create(
      <Feature flag="hello" envs={[ENVS.LOCAL]}>
        Hello
      </Feature>,
    )

    expect(component.toJSON()).toEqual(null)

    act(() => {
      component.unmount()
    })
  })

  it('should render an enabled feature', () => {
    localStorage.setItem('FeatureFlags', JSON.stringify({ hello: true }))

    const component = TestRenderer.create(
      <Feature flag="hello" envs={[ENVS.LOCAL]}>
        Hello
      </Feature>,
    )

    expect(component.toJSON()).toEqual('Hello')

    act(() => {
      component.unmount()
    })
  })

  it('should render a feature with no env in localdev', () => {
    const component = TestRenderer.create(
      <Feature flag="hello" envs={[]}>
        Hello
      </Feature>,
    )

    expect(component.toJSON()).toEqual('Hello')
  })

  it('should not render a feature with no env in prod', () => {
    require('next/config').__setPublicRuntimeConfig({
      ENVIRONMENT: 'prod',
    })

    const component = TestRenderer.create(
      <Feature flag="hello" envs={[]}>
        Hello
      </Feature>,
    )

    expect(component.toJSON()).toEqual(null)
  })

  it('should render a prod feature in prod', () => {
    require('next/config').__setPublicRuntimeConfig({
      ENVIRONMENT: 'prod',
    })

    const component = TestRenderer.create(
      <Feature flag="hello" envs={[ENVS.PROD]}>
        Hello
      </Feature>,
    )

    expect(component.toJSON()).toEqual('Hello')
  })
})
