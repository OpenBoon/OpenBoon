import TestRenderer, { act } from 'react-test-renderer'

import asset from '../../Asset/__mocks__/asset'
import models from '../../Models/__mocks__/models'
import project from '../../Project/__mocks__/project'

import AssetLabeling from '..'

const PROJECT_ID = project.id
const ASSET_ID = asset.id
const MODEL_ID = models.results[0].id
const ALT_MODEL_ID = models.results[1].id

const noop = () => () => {}

jest.mock('../Header', () => 'AssetLabelingHeader')

describe('<AssetLabeling />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<AssetLabeling />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render with a selected asset', async () => {
    require('swr').__setMockUseSWRResponse({ data: models })
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, id: ASSET_ID },
    })

    const component = TestRenderer.create(<AssetLabeling />)

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ children: 'Create New Model' })
        .props.onClick({ stopPropagation: noop })
    })

    act(() => {
      component.root
        .findByProps({ label: 'Model' })
        .props.onChange({ value: MODEL_ID })
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ id: 'asset-label' })
        .props.onChange({ target: { value: 'Flimflarm' } })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Mock Failure
    fetch.mockResponseOnce(
      JSON.stringify({ label: ['I guess you cannot use this label'] }),
      {
        status: 400,
      },
    )

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ type: 'submit' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Unknown Failure
    fetch.mockRejectOnce(null, { status: 500 })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ type: 'submit' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ detail: 'Label Saved' }))

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ type: 'submit' })
        .props.onClick({ preventDefault: noop })
    })

    expect(fetch.mock.calls.length).toEqual(3)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/models/${MODEL_ID}/add_labels/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: JSON.stringify({
        add_labels: [
          {
            assetId: ASSET_ID,
            label: 'Flimflarm',
          },
        ],
      }),
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ label: 'Model' })
        .props.onChange({ value: ALT_MODEL_ID })
      component.root
        .findByProps({ id: 'asset-label' })
        .props.onChange({ target: { value: 'FlamFlirm' } })
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ children: 'Cancel' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
