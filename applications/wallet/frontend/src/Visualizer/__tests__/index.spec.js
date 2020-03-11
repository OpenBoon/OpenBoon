import TestRenderer from 'react-test-renderer'

import Visualizer from '..'

import assets from '../../Assets/__mocks__/assets'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<Visualizer />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({
      data: assets,
    })

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(<Visualizer />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render selected asset', () => {
    require('swr').__setMockUseSWRResponse({
      data: assets,
    })

    require('next/router').__setUseRouter({
      query: { id: 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C' },
    })

    const component = TestRenderer.create(<Visualizer />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
