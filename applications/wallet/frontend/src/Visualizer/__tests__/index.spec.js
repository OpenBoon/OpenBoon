import TestRenderer from 'react-test-renderer'

import assets from '../../Assets/__mocks__/assets'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import Visualizer from '..'

jest.mock('../../Metadata', () => 'Metadata')
jest.mock('../../Assets/QuickView', () => 'AssetsQuickView')

jest.mock('../Navigation', () => 'VisualizerNavigation')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = assets.results[0].id

describe('<Visualizer />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRInfiniteResponse({ data: [assets] })

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, action: 'delete-asset-success' },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Visualizer />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render selected asset', () => {
    require('swr').__setMockUseSWRInfiniteResponse({ data: [assets] })

    require('next/router').__setUseRouter({
      query: { assetId: ASSET_ID, projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Visualizer />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
