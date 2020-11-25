import TestRenderer, { act } from 'react-test-renderer'

import model from '../../Model/__mocks__/model'

import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import ModelMatrix from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'

describe('<ModelMatrix />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]/matrix',
      query: {
        projectId: PROJECT_ID,
        modelId: MODEL_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({ data: model })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <ModelMatrix />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // useEffect
    act(() => {})
  })
})
