import TestRenderer, { act } from 'react-test-renderer'

import JobErrorJobMenu from '..'

import job from '../../Job/__mocks__/job'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = '223fd17d-7028-1519-94a8-d2f0132bc0c8'

describe('<JobErrorJobMenu />', () => {
  it('should render properly', async () => {
    fetch.mockResponseOnce('{}')

    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <JobErrorJobMenu
        projectId={PROJECT_ID}
        jobId={JOB_ID}
        revalidate={mockFn}
      />,
    )

    act(() => {
      component.root.findByProps({ 'aria-label': 'Toggle Error Actions Menu' })
    })
    expect(component.toJSON()).toMatchSnapshot()
  })
})
