import TestRenderer, { act } from 'react-test-renderer'

import permissions from '../../Permissions/__mocks__/permissions'

import DataSourcesAddForm from '../Form'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<DataSourcesAddForm />', () => {
  it('should render properly after permissions are loaded', async () => {
    const mockFn = jest.fn()

    require('next/router').__setMockPushFunction(mockFn)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/data-sources/add',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: permissions,
    })

    const component = TestRenderer.create(<DataSourcesAddForm />)

    expect(component.toJSON()).toMatchSnapshot()

    // Input email
    act(() => {
      component.root
        .findByProps({ id: 'name' })
        .props.onChange({ target: { value: 'My Data Source' } })
    })

    // Input url
    act(() => {
      component.root
        .findByProps({ id: 'uri' })
        .props.onChange({ target: { value: 'gs://zorroa-dev-data' } })
    })

    // Input credential
    act(() => {
      component.root
        .findByProps({ id: 'credential' })
        .props.onChange({ target: { value: 'jkdT9Uherdozguie89FHIJS' } })
    })

    // Select file type
    act(() => {
      component.root
        .findByProps({ type: 'checkbox', value: 'images' })
        .props.onClick()
    })

    // Select module
    act(() => {
      component.root
        .findByProps({ type: 'checkbox', value: 'zmlp-classification' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Failure
    fetch.mockResponseOnce(JSON.stringify({ name: ['Name already in use'] }), {
      status: 400,
    })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Create Data Source' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ detail: 'Data Source Created' }))

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Create Data Source' })
        .props.onClick({ preventDefault: noop })
    })

    expect(fetch.mock.calls.length).toEqual(2)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/datasources/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: JSON.stringify({
        name: 'My Data Source',
        uri: 'gs://zorroa-dev-data',
        credential: 'jkdT9Uherdozguie89FHIJS',
        file_types: ['gif', ' png', ' jpg', ' jpeg', ' tif', ' tiff', ' psd'],
        modules: ['zmlp-classification'],
      }),
    })

    expect(mockFn).toHaveBeenCalledWith(
      '/[projectId]/data-sources?action=add-datasource-success',
      `/${PROJECT_ID}/data-sources?action=add-datasource-success`,
    )
  })
})
