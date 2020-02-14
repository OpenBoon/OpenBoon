import TestRenderer from 'react-test-renderer'

import DataSources from '..'

import dataSources from '../__mocks__/dataSources'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<DataSources />', () => {
  it('should render properly while loading', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/data-sources',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {},
    })

    const component = TestRenderer.create(<DataSources />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with no data sources', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/data-sources',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 0,
        next: null,
        previous: null,
        results: [],
      },
    })

    const component = TestRenderer.create(<DataSources />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with data sources', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/data-sources',
      query: { projectId: PROJECT_ID, action: 'add-datasource-success' },
    })

    require('swr').__setMockUseSWRResponse({
      data: dataSources,
    })

    const component = TestRenderer.create(<DataSources />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
