import TestRenderer, { act } from 'react-test-renderer'

import dataSource from '../../DataSource/__mocks__/dataSource'

import DataSourcesEdit from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATA_SOURCE_ID = '2f0de857-95fd-120e-85f3-0242ac120002'

const noop = () => () => {}

describe('<DataSourcesEdit />', () => {
  it('should render properly while loading', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/data-sources/add',
      query: { projectId: PROJECT_ID, dataSourceId: DATA_SOURCE_ID },
    })

    const component = TestRenderer.create(<DataSourcesEdit />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with a data source', async () => {
    const mockFn = jest.fn()

    require('next/router').__setMockPushFunction(mockFn)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/data-sources/[dataSourceId]/edit',
      query: { projectId: PROJECT_ID, dataSourceId: DATA_SOURCE_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: dataSource,
    })

    const component = TestRenderer.create(<DataSourcesEdit />)

    expect(component.toJSON()).toMatchSnapshot()

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
        .findByProps({ children: 'Save' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ detail: 'Data Source Created' }))

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Save' })
        .props.onClick({ preventDefault: noop })
    })

    expect(fetch.mock.calls.length).toEqual(2)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/datasources/${DATA_SOURCE_ID}/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: JSON.stringify({
        file_types: ['gif', ' png', ' jpg', ' jpeg', ' tif', ' tiff', ' psd'],
        modules: ['zmlp-classification'],
      }),
    })

    expect(mockFn).toHaveBeenCalledWith(
      '/[projectId]/data-sources?action=edit-datasource-success',
      `/${PROJECT_ID}/data-sources?action=edit-datasource-success`,
    )
  })
})
