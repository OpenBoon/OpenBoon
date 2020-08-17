import TestRenderer from 'react-test-renderer'

import FilterIcon from '../Icon'

describe('<FilterIcon />', () => {
  it('should render properly', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <FilterIcon filter={{ attribute: 'clip.length', type: 'range' }} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
