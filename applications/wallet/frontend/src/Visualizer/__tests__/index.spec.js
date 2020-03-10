import TestRenderer from 'react-test-renderer'

import Visualizer from '..'

import assets from '../../Assets/__mocks__/assets'

describe('<Visualizer />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({
      data: assets,
    })

    const component = TestRenderer.create(<Visualizer />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
