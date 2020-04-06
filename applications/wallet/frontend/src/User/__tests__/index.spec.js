import TestRenderer from 'react-test-renderer'

import User from '..'

describe('<User />', () => {
  it('should return null while the user is loading', () => {
    require('swr').__setMockUseSWRResponse({ data: null })

    const component = TestRenderer.create(
      <User initialUser={{}}>Hello world!</User>,
    )

    expect(component.toJSON()).toBe(null)
  })
})
