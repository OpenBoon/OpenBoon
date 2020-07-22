import TestRenderer from 'react-test-renderer'

import modelLabels from '../__mocks__/modelLabels'

import ModelLabels from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'

describe('<ModelLabels />', () => {
  it('should render properly with no labels', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 0,
        next: null,
        previous: null,
        results: [],
      },
    })

    const component = TestRenderer.create(<ModelLabels />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with labels', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: modelLabels })

    const component = TestRenderer.create(<ModelLabels />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
