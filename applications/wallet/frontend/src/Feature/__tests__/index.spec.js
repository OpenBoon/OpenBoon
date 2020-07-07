import TestRenderer, { act } from 'react-test-renderer'

import Feature from '..'

describe('<Feature />', () => {
  it('should not render a feature with no env', () => {
    const component = TestRenderer.create(
      <Feature flag="hello" env={[]}>
        Hello
      </Feature>,
    )

    expect(component.toJSON()).toEqual(null)
  })

  it('should not render a prod feature in localdev env', () => {
    const component = TestRenderer.create(
      <Feature flag="hello" env={['zvi-prod']}>
        Hello
      </Feature>,
    )

    expect(component.toJSON()).toEqual(null)
  })

  it('should render a localdev feature in localdev env', () => {
    const component = TestRenderer.create(
      <Feature flag="hello" env={['localdev']}>
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
      <Feature flag="hello" env={['zvi-prod']}>
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
      <Feature flag="hello" env={['localdev']}>
        Hello
      </Feature>,
    )

    // useEffect
    act(() => {})

    expect(component.toJSON()).toEqual(null)
  })
})
