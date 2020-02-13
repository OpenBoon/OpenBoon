import TestRenderer, { act } from 'react-test-renderer'

import permissions from '../../Permissions/__mocks__/permissions'

import DataSourcesAddForm from '../Form'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<DataSourcesAddForm />', () => {
  it('should render properly after permissions are loaded', () => {
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
        .findByProps({ id: 'url' })
        .props.onChange({ target: { value: 'gs://zorroa-dev-data' } })
    })

    // Input key
    act(() => {
      component.root
        .findByProps({ id: 'key' })
        .props.onChange({ target: { value: 'jkdT9Uherdozguie89FHIJS' } })
    })

    // Select file type
    act(() => {
      component.root
        .findByProps({ type: 'checkbox', value: 'Image Files' })
        .props.onClick()
    })

    // Select module
    act(() => {
      component.root
        .findByProps({ type: 'checkbox', value: 'label-detection' })
        .props.onClick({ preventDefault: noop })
    })

    act(() => {
      component.root
        .findByProps({ children: 'Create Data Source' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
