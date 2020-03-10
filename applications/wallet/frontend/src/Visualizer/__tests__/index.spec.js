import TestRenderer from 'react-test-renderer'

import Visualizer from '..'

import assets from '../../Assets/__mocks__/assets'

describe('<Visualizer />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({
      data: assets,
    })

    require('next/router').__setUseRouter({
      query: { projectId: '00000000-0000-0000-0000-000000000000' },
    })

    const component = TestRenderer.create(<Visualizer />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
