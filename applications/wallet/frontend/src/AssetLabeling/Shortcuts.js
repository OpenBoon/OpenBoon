import { useCallback, useEffect } from 'react'

import { onSubmit } from './helpers'

const AssetLabelingShortcuts = ({
  dispatch,
  state,
  labels,
  projectId,
  assetId,
}) => {
  const keydownHandler = useCallback(
    (event) => {
      const {
        code,
        target: { tagName },
      } = event

      /* istanbul ignore next */
      if (['INPUT', 'TEXTAREA'].includes(tagName)) return

      if (code === 'KeyS') {
        onSubmit({ dispatch, state, labels, projectId, assetId })
      }
    },
    [dispatch, state, labels, projectId, assetId],
  )

  useEffect(() => {
    document.addEventListener('keydown', keydownHandler)

    return () => document.removeEventListener('keydown', keydownHandler)
  }, [keydownHandler])

  return null
}

export default AssetLabelingShortcuts
