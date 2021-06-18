import TestRenderer, { act } from 'react-test-renderer'

import TimelineFilterTracks from '../FilterTracks'

describe('<TimelineFilterTracks />', () => {
  it('should render properly', () => {
    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <TimelineFilterTracks filter="" width={200} dispatch={mockDispatch} />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Filter tracks' })
        .props.onChange({ value: 'cat' })
    })

    expect(mockDispatch).toHaveBeenCalledWith({
      payload: { value: 'cat' },
      type: 'UPDATE_FILTER',
    })
  })
})
