import { useCallback, useEffect } from 'react'

const AssetLabelingShortcuts = ({ onSubmit }) => {
  const keydownHandler = useCallback(
    (event) => {
      const {
        code,
        target: { tagName },
      } = event

      /* istanbul ignore next */
      if (['INPUT', 'TEXTAREA'].includes(tagName)) return

      if (code === 'KeyS') {
        onSubmit()
      }
    },
    [onSubmit],
  )

  useEffect(() => {
    document.addEventListener('keydown', keydownHandler)

    return () => document.removeEventListener('keydown', keydownHandler)
  }, [keydownHandler])

  return null
}

export default AssetLabelingShortcuts
