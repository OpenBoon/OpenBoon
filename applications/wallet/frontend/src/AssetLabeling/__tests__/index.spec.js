import TestRenderer, { act } from 'react-test-renderer'

import asset from '../../Asset/__mocks__/asset'
import datasets from '../../Datasets/__mocks__/datasets'

import AssetLabeling from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'

jest.mock('../Form', () => 'AssetLabelingForm')
jest.mock('../../BulkAssetLabeling', () => 'BulkAssetLabeling')

describe('AssetLabeling', () => {
  it('should render properly with no asset selected', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(<AssetLabeling />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with bulk labeling selected', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(<AssetLabeling />)

    act(() => {
      component.root
        .findByProps({ children: 'Label All Assets in Search' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with bulk labeling selected', async () => {
    const query = btoa(
      JSON.stringify([
        {
          type: 'limit',
          attribute: 'utility.Search Results Limit',
          values: { maxAssets: 10_000 },
        },
        {
          type: 'facet',
          attribute: 'media.type',
          values: { facets: ['image', 'document'] },
        },
      ]),
    )

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, query },
    })

    const component = TestRenderer.create(<AssetLabeling />)

    act(() => {
      component.root
        .findByProps({ children: 'Label All Assets in Search' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with an asset selected', async () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({
      data: {
        id: asset.id,
        metadata: {
          source: asset.metadata.source,
        },
        ...datasets,
      },
    })

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const component = TestRenderer.create(<AssetLabeling />)

    expect(component.toJSON()).toMatchSnapshot()

    // Select Unknown Dataset
    act(() => {
      component.root
        .findByProps({ label: 'Dataset:' })
        .props.onChange({ value: 'Unknown' })
    })

    // Select FaceRecognition Dataset
    act(() => {
      component.root
        .findByProps({ label: 'Dataset:' })
        .props.onChange({ value: datasets.results[0].id })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Open Menu
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Actions Menu' })
        .props.onClick()
    })

    // Select Add Dataset Filter
    act(() => {
      component.root
        .findByProps({ children: 'Add Dataset Filter' })
        .props.onClick()
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'label',
          attribute: 'labels.faces',
          datasetId: datasets.results[0].id,
          values: { labels: [] },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?assetId=${ASSET_ID}&query=${query}`,
      `/${PROJECT_ID}/visualizer?assetId=${ASSET_ID}&query=${query}`,
    )
  })
})
