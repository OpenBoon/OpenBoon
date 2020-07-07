import TestRenderer, { act } from 'react-test-renderer'

import Feature, { ENVS } from '..'

describe('<Feature />', () => {
  it('should not render a feature with no env', () => {
    const component = TestRenderer.create(
      <Feature flag="hello" envs={[]}>
        Hello
      </Feature>,
    )

    expect(component.toJSON()).toEqual(null)
  })

  it('should not render a prod feature in localdev env', () => {
    const component = TestRenderer.create(
      <Feature flag="hello" envs={[ENVS.PROD]}>
        Hello
      </Feature>,
    )

    expect(component.toJSON()).toEqual(null)
  })

  it('should render a localdev feature in localdev env', () => {
    const component = TestRenderer.create(
      <Feature flag="hello" envs={[ENVS.LOCAL]}>
        Hello
      </Feature>,
    )

    expect(component.toJSON()).toEqual('Hello')
  })

  it('should render a prod feature in localdev env with the query param enabled', () => {
    require('next/router').__setUseRouter({
      query: { flags: 'hello:true' },
    })

    const component = TestRenderer.create(
      <Feature flag="hello" envs={[ENVS.PROD]}>
        Hello
      </Feature>,
    )

    // useEffect
    act(() => {})

    expect(component.toJSON()).toEqual('Hello')
  })

  it('should not render a localdev feature in localdev env with the query param disabled', () => {
    require('next/router').__setUseRouter({
      query: { flags: 'hello:false' },
    })

    const component = TestRenderer.create(
      <Feature flag="hello" envs={[ENVS.LOCAL]}>
        Hello
      </Feature>,
    )

    // useEffect
    act(() => {})

    expect(component.toJSON()).toEqual(null)
  })
})
