import TestRenderer, { act } from 'react-test-renderer'

import providers from '../../Providers/__mocks__/providers'
import dataSource from '../../DataSource/__mocks__/dataSource'

import DataSourcesEditForm from '../Form'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATA_SOURCE_ID = dataSource.id
const MODULE = providers.results[0].categories[0].modules[0]

describe('<DataSourcesEditForm />', () => {
  it('should render properly', async () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const mockScrollTo = jest.fn()

    Object.defineProperty(global.window, 'scrollTo', { value: mockScrollTo })

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/data-sources/add',
      query: { projectId: PROJECT_ID, dataSourceId: DATA_SOURCE_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: providers })

    const component = TestRenderer.create(
      <DataSourcesEditForm
        initialState={{
          name: dataSource.name,
          uri: dataSource.uri,
          fileTypes: { Videos: true },
          modules: [MODULE.id],
          errors: { global: '' },
        }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Update name
    act(() => {
      component.root
        .findByProps({ id: 'name' })
        .props.onChange({ target: { value: 'My Updated Data Source' } })
    })

    // Select file type
    act(() => {
      component.root
        .findByProps({ type: 'checkbox', value: 'Images' })
        .props.onClick()
    })

    // Select module
    act(() => {
      component.root
        .findByProps({ type: 'checkbox', value: 'boonai-label-detection' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Failure
    fetch.mockResponseOnce(JSON.stringify({ name: ['Name already in use'] }), {
      status: 400,
    })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Update Data Source' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockScrollTo).toHaveBeenCalledWith(0, 0)
    mockScrollTo.mockClear()

    // Mock Unknown Failure
    fetch.mockRejectOnce(null, { status: 500 })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Update Data Source' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ detail: 'Data Source Created' }))

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Update Data Source' })
        .props.onClick({ preventDefault: noop })
    })

    expect(fetch.mock.calls.length).toEqual(5)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/data_sources/${DATA_SOURCE_ID}/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: JSON.stringify({
        name: 'My Updated Data Source',
        uri: 'gs://zorroa-dev-data/images',
        fileTypes: ['Videos', 'Images'],
        modules: [MODULE.name, 'boonai-label-detection'],
      }),
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      '/[projectId]/data-sources?action=edit-datasource-success',
      `/${PROJECT_ID}/data-sources`,
    )
  })

  it('should display an error with an empty source name', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/data-sources/add',
      query: { projectId: PROJECT_ID, dataSourceId: DATA_SOURCE_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: providers })

    const component = TestRenderer.create(
      <DataSourcesEditForm
        initialState={{
          name: '',
          uri: dataSource.uri,
          fileTypes: { Videos: true },
          modules: [MODULE.id],
          errors: { global: '' },
        }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
