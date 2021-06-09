import TestRenderer from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'
import datasets from '../__mocks__/datasets'
import datasetTypes from '../../DatasetTypes/__mocks__/datasetTypes'

import User from '../../User'

import DatasetsTable from '../Table'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<DatasetsTable />', () => {
  it('should render properly without datasets', async () => {
    require('next/router').__setUseRouter({
      pathname: `/[projectId]/datasets`,
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: {} })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <DatasetsTable
          projectId={PROJECT_ID}
          datasetTypes={datasetTypes.results}
        />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with datasets', async () => {
    require('next/router').__setUseRouter({
      pathname: `/[projectId]/datasets`,
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: datasets })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <DatasetsTable
          projectId={PROJECT_ID}
          datasetTypes={datasetTypes.results}
        />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
