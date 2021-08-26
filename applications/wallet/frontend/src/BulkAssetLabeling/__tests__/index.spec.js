import TestRenderer, { act } from 'react-test-renderer'

import datasets from '../../Datasets/__mocks__/datasets'

import BulkAssetLabeling from '..'
import BulkAssetLabelingForm from '../Form'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../Form', () => 'BulkAssetLabelingForm')

const noop = () => () => {}

describe('BulkAssetLabeling', () => {
  it('should render properly', async () => {
    const mockFn = jest.fn()

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: datasets })

    const component = TestRenderer.create(
      <BulkAssetLabeling
        projectId={PROJECT_ID}
        query=""
        setIsBulkLabeling={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Select Dataset
    act(() => {
      component.root
        .findByProps({ label: 'Dataset:' })
        .props.onChange({ value: datasets.results[0].id })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Fake label dispatch
    act(() => {
      component.root
        .findByType(BulkAssetLabelingForm)
        .props.dispatch({ lastLabel: 'cat' })
    })

    // Mock Unknown Failure
    fetch.mockRejectOnce(null, { status: 500 })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Save' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ detail: 'Label Edited' }))

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Save' })
        .props.onClick({ preventDefault: noop })
    })

    expect(fetch.mock.calls.length).toEqual(3)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/datasets/${datasets.results[0].id}/add_labels_by_search_filters/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: JSON.stringify({
        filters: [],
        label: 'cat',
        scope: 'TRAIN',
      }),
    })

    // Click Cancel
    await act(async () => {
      component.root
        .findByProps({ children: 'Cancel' })
        .props.onClick({ preventDefault: noop })
    })
  })
})
