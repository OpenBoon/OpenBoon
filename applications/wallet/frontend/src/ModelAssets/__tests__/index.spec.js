import TestRenderer from 'react-test-renderer'

import model from '../../Model/__mocks__/model'
import mockUser from '../../User/__mocks__/user'
import labels from '../../ModelLabels/__mocks__/modelLabels'

import User from '../../User'

import ModelAssets from '..'

jest.mock('../../Breadcrumbs', () => 'Breadcrumbs')
jest.mock('../Content', () => 'ModelAssetsContent')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'

describe('<ModelAssets />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]/assets',
      query: {
        projectId: PROJECT_ID,
        modelId: MODEL_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({ data: { ...model, ...labels } })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <ModelAssets
          projectId={PROJECT_ID}
          modelId={MODEL_ID}
          moduleName="Module"
        />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
