let mockCopyFn = () => {}

export const __setMockCopyFn = (fn) => {
  mockCopyFn = fn
}

const useClipboard = (value) => [undefined, () => mockCopyFn(value)]

export default useClipboard
