import TestRenderer, { act } from 'react-test-renderer'

import asset from '../../Asset/__mocks__/asset'
import datasets from '../../Datasets/__mocks__/datasets'

import AssetLabeling from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'

jest.mock('../Form', () => 'AssetLabelingForm')

describe('AssetLabeling', () => {
  it('should render properly with no asset selected', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(<AssetLabeling />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with an asset selected', async () => {
    require('swr').__setMockUseSWRResponse({ data: { ...asset, ...datasets } })

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
  })
})
