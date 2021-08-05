import TestRenderer from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'
import model from '../../Model/__mocks__/model'

import User from '../../User'

import ModelLink from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf774-89d9-1244-9596-d6df43f1ede5'

jest.mock('../Form', () => 'ModelLinkForm')

describe('<ModelLink />', () => {
  it('should render properly for Existing', async () => {
    const mockFn = jest.fn()

    require('next/router').__setMockPushFunction(mockFn)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]/link',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <ModelLink model={model} />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
