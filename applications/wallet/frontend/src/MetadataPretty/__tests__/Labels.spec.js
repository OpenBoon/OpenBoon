import TestRenderer from 'react-test-renderer'

import datasets from '../../Datasets/__mocks__/datasets'
import asset from '../../Asset/__mocks__/asset'

import MetadataPrettyLabels from '../Labels'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = asset.id

jest.mock('../LabelsContent', () => 'MetadataPrettyLabelsContent')

describe('<MetadataPrettyLabels />', () => {
  it('should render properly on the grid', () => {
    require('swr').__setMockUseSWRResponse({ data: datasets })

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, id: ASSET_ID },
    })

    const component = TestRenderer.create(
      <MetadataPrettyLabels labels={asset.metadata.labels} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly for a single asset', () => {
    require('swr').__setMockUseSWRResponse({ data: datasets })

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const component = TestRenderer.create(
      <MetadataPrettyLabels labels={asset.metadata.labels} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
