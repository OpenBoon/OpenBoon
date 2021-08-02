import TestRenderer from 'react-test-renderer'

import ModelUploadProgress from '../Progress'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'

const noop = () => () => {}

describe('<ModelUploadProgress />', () => {
  it('should render properly when it has failed', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    const component = TestRenderer.create(
      <ModelUploadProgress
        state={{
          file: {
            name: 'model.zip',
            size: 123456789,
          },
          isConfirmed: true,
          progress: 50,
          hasFailed: true,
          request: {
            abort: noop,
          },
        }}
        dispatch={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when it has succeeded', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    const component = TestRenderer.create(
      <ModelUploadProgress
        state={{
          file: {
            name: 'model.zip',
            size: 123456789,
          },
          isConfirmed: true,
          progress: 100,
          hasFailed: false,
          request: {
            abort: noop,
          },
        }}
        dispatch={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
