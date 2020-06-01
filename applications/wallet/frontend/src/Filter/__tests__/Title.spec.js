import TestRenderer from 'react-test-renderer'

import FilterTitle from '../Title'

describe('<FilterTitle />', () => {
  it('should render properly', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <FilterTitle filter={{ attribute: 'clip.length', type: 'range' }} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
