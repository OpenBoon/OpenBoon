import TestRenderer from 'react-test-renderer'

import modelTypes from '../../ModelTypes/__mocks__/modelTypes'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import Model from '..'

jest.mock('../../Breadcrumbs', () => 'Breadcrumbs')
jest.mock('../Details', () => 'ModelDetails')
jest.mock('../MatrixLink', () => 'ModelMatrixLink')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'

describe('<Model />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: {
        projectId: PROJECT_ID,
        modelId: MODEL_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({ data: modelTypes })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Model />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
