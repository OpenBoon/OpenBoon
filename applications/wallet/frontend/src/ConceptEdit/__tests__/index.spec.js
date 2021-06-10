import TestRenderer, { act } from 'react-test-renderer'

import ConceptEdit from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATASET_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'

const noop = () => () => {}

describe('<ConceptEdit />', () => {
  it('should render properly', async () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <ConceptEdit projectId={PROJECT_ID} datasetId={DATASET_ID} label="cat" />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Update label
    act(() => {
      component.root
        .findByProps({ id: 'newLabel' })
        .props.onChange({ target: { value: 'dog' } })
    })

    // Mock Failure
    fetch.mockResponseOnce(JSON.stringify({ name: ['Name already in use'] }), {
      status: 400,
    })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Save Concept Changes' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Unknown Failure
    fetch.mockRejectOnce(null, { status: 500 })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Save Concept Changes' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ detail: 'Label Edited' }))

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Save Concept Changes' })
        .props.onClick({ preventDefault: noop })
    })

    expect(fetch.mock.calls.length).toEqual(4)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/datasets/${DATASET_ID}/rename_label/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: JSON.stringify({
        label: 'cat',
        newLabel: 'dog',
      }),
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      '/[projectId]/datasets/[datasetId]?action=edit-concept-success',
      `/${PROJECT_ID}/datasets/${DATASET_ID}`,
    )
  })
})
