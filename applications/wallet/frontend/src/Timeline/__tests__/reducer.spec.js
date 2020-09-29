import timelines from '../__mocks__/timelines'

import { reducer, ACTIONS, INITIAL_STATE } from '../reducer'

describe('<Timeline /> reducer', () => {
  it('should return the state', () => {
    expect(reducer(INITIAL_STATE, {})).toEqual(INITIAL_STATE)
  })

  it('should update the filter', () => {
    expect(
      reducer(INITIAL_STATE, {
        type: ACTIONS.UPDATE_FILTER,
        payload: { value: 'cat' },
      }),
    ).toEqual({ filter: 'cat', modules: {} })
  })

  it('should open an undefined module', () => {
    expect(
      reducer(INITIAL_STATE, {
        type: ACTIONS.TOGGLE_OPEN,
        payload: { timeline: 'gcp-video-logo-detection' },
      }),
    ).toEqual({
      filter: '',
      modules: { 'gcp-video-logo-detection': { isOpen: true } },
    })
  })

  it('should open a closed module', () => {
    expect(
      reducer(
        {
          filter: '',
          modules: { 'gcp-video-logo-detection': { isOpen: false } },
        },
        {
          type: ACTIONS.TOGGLE_OPEN,
          payload: { timeline: 'gcp-video-logo-detection' },
        },
      ),
    ).toEqual({
      filter: '',
      modules: { 'gcp-video-logo-detection': { isOpen: true } },
    })
  })

  it('should close an open module', () => {
    expect(
      reducer(
        {
          filter: '',
          modules: { 'gcp-video-logo-detection': { isOpen: true } },
        },
        {
          type: ACTIONS.TOGGLE_OPEN,
          payload: { timeline: 'gcp-video-logo-detection' },
        },
      ),
    ).toEqual({
      filter: '',
      modules: { 'gcp-video-logo-detection': { isOpen: false } },
    })
  })

  it('should hide an undefined module', () => {
    expect(
      reducer(INITIAL_STATE, {
        type: ACTIONS.TOGGLE_VISIBLE,
        payload: { timeline: 'gcp-video-logo-detection' },
      }),
    ).toEqual({
      filter: '',
      modules: { 'gcp-video-logo-detection': { isVisible: false } },
    })
  })

  it('should hide a visible module', () => {
    expect(
      reducer(
        {
          filter: '',
          modules: { 'gcp-video-logo-detection': { isVisible: true } },
        },
        {
          type: ACTIONS.TOGGLE_VISIBLE,
          payload: { timeline: 'gcp-video-logo-detection' },
        },
      ),
    ).toEqual({
      filter: '',
      modules: { 'gcp-video-logo-detection': { isVisible: false } },
    })
  })

  it('should show a hidden module', () => {
    expect(
      reducer(
        {
          filter: '',
          modules: { 'gcp-video-logo-detection': { isVisible: false } },
        },
        {
          type: ACTIONS.TOGGLE_VISIBLE,
          payload: { timeline: 'gcp-video-logo-detection' },
        },
      ),
    ).toEqual({
      filter: '',
      modules: { 'gcp-video-logo-detection': { isVisible: true } },
    })
  })

  it('should show all modules when one of them is hidden', () => {
    expect(
      reducer(
        {
          filter: '',
          modules: { 'gcp-video-logo-detection': { isVisible: false } },
        },
        {
          type: ACTIONS.TOGGLE_VISIBLE_ALL,
          payload: { timelines },
        },
      ),
    ).toEqual({
      filter: '',
      modules: {
        'gcp-video-explicit-detection': { isVisible: true },
        'gcp-video-label-detection': { isVisible: true },
        'gcp-video-logo-detection': { isVisible: true },
        'gcp-video-object-detection': { isVisible: true },
        'gcp-video-text-detection': { isVisible: true },
      },
    })
  })

  it('should hide all modules when all of them are visible', () => {
    expect(
      reducer(
        {
          filter: '',
          modules: { 'gcp-video-logo-detection': { isVisible: true } },
        },
        {
          type: ACTIONS.TOGGLE_VISIBLE_ALL,
          payload: { timelines },
        },
      ),
    ).toEqual({
      filter: '',
      modules: {
        'gcp-video-explicit-detection': { isVisible: false },
        'gcp-video-label-detection': { isVisible: false },
        'gcp-video-logo-detection': { isVisible: false },
        'gcp-video-object-detection': { isVisible: false },
        'gcp-video-text-detection': { isVisible: false },
      },
    })
  })
})
