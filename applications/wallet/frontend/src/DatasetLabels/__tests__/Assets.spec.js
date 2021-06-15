import TestRenderer, { act } from 'react-test-renderer'

import assets from '../../Assets/__mocks__/assets'

import { encode } from '../../Filters/helpers'

import DatasetLabelsAssets from '../Assets'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATASET_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'
const ASSET_ID = assets.results[0].id

describe('<DatasetLabelsAssets />', () => {
  it('should render properly', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/datasets/[datasetId]/labels',
      query: {
        projectId: PROJECT_ID,
        datasetId: DATASET_ID,
      },
    })

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({ data: assets })

    const component = TestRenderer.create(
      <DatasetLabelsAssets
        projectId={PROJECT_ID}
        datasetId={DATASET_ID}
        page={1}
        datasetName="cats"
        scope="TRAIN"
        label="tabby"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Mock Failure
    fetch.mockRejectOnce({
      json: () => Promise.resolve({ label: ['Error message'] }),
    })

    await act(async () => {
      component.root
        .findAllByProps({ title: 'Remove from set' })[0]
        .props.onClick()
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({}))

    await act(async () => {
      component.root
        .findAllByProps({ title: 'Remove from set' })[0]
        .props.onClick()
    })

    expect(fetch.mock.calls.length).toEqual(3)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/datasets/${DATASET_ID}/delete_labels/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: `{"removeLabels":[{"assetId":"${ASSET_ID}","label":"tabby"}]}`,
    })

    expect(fetch.mock.calls[2][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/searches/query/?query=${encode({
        filters: [
          {
            type: 'label',
            attribute: 'labels.cats',
            datasetId: DATASET_ID,
            values: {
              scope: 'TRAIN',
              labels: ['tabby'],
            },
          },
        ],
      })}&from=0&size=32`,
    )
  })

  it('should render properly', async () => {
    require('swr').__setMockUseSWRResponse({})

    const component = TestRenderer.create(
      <DatasetLabelsAssets
        projectId={PROJECT_ID}
        datasetId={DATASET_ID}
        page={1}
        datasetName="cats"
        scope="TRAIN"
        label="tabby"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
