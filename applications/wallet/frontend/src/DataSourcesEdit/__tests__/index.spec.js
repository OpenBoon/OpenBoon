import TestRenderer from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'

import dataSource from '../../DataSource/__mocks__/dataSource'

import User from '../../User'

import DataSourcesEdit from '..'

jest.mock('../Form', () => 'DataSourcesEditForm')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATA_SOURCE_ID = dataSource.id

describe('<DataSourcesEdit />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/data-sources/add',
      query: { projectId: PROJECT_ID, dataSourceId: DATA_SOURCE_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: dataSource })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <DataSourcesEdit />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
