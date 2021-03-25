import TestRenderer, { act } from 'react-test-renderer'

import organizations from '../__mocks__/organizations'

import Organizations from '..'

describe('<Organizations />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({ data: organizations })

    const component = TestRenderer.create(<Organizations />)

    act(() => {
      component.root
        .findByProps({ label: 'Sort by' })
        .props.onChange({ value: 'date' })
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ type: 'search' })
        .props.onChange({ target: { value: 'erty' } })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with no organizations', () => {
    require('swr').__setMockUseSWRResponse({ data: { results: [] } })

    const component = TestRenderer.create(<Organizations />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
