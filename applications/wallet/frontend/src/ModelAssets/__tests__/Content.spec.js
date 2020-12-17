import TestRenderer, { act } from 'react-test-renderer'

import assets from '../../Assets/__mocks__/assets'

import { encode } from '../../Filters/helpers'

import ModelAssetsContent from '../Content'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf774-89d9-1244-9596-d6df43f1ede5'
const ASSET_ID = assets.results[0].id

const ENCODED_FILTER = encode({
  filters: [
    {
      type: 'label',
      attribute: 'labels.TestModule',
      modelId: MODEL_ID,
      values: {
        scope: 'train',
        labels: ['Label'],
      },
    },
  ],
})

describe('<ModelAssets />', () => {
  it('should render properly', async () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: assets })

    const component = TestRenderer.create(
      <ModelAssetsContent label="Label" filter={ENCODED_FILTER} />,
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
      `/api/v1/projects/${PROJECT_ID}/models/${MODEL_ID}/delete_labels/`,
    )
    expect(fetch.mock.calls[0][1]).toEqual({
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: `{"removeLabels":[{"assetId":"${ASSET_ID}","label":"Label"}]}`,
    })

    expect(fetch.mock.calls[1][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/models/${MODEL_ID}/delete_labels/`,
    )
    expect(fetch.mock.calls[1][1]).toEqual({
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: `{"removeLabels":[{"assetId":"${ASSET_ID}","label":"Label"}]}`,
    })

    expect(fetch.mock.calls[2][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/searches/query/?query=${ENCODED_FILTER}&from=0&size=32`,
    )
  })

  it('should render properly without data', () => {
    require('next/router').__setUseRouter({
      query: { action: 'remove-asset-success' },
    })

    require('swr').__setMockUseSWRResponse({})

    const component = TestRenderer.create(
      <ModelAssetsContent
        label="Label"
        filter={encode({
          filters: [
            {
              type: 'label',
              attribute: 'labels.TestModule',
              modelId: MODEL_ID,
              values: {
                scope: 'train',
                labels: ['Label'],
              },
            },
          ],
        })}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
