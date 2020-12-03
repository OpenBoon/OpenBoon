let mockIsCopied = false

export const __setMockIsCopied = (bool) => {
  mockIsCopied = bool
}

let mockCopyFn = () => {}

export const __setMockCopyFn = (fn) => {
  mockCopyFn = fn
}

const useClipboard = (value) => [mockIsCopied, () => mockCopyFn(value)]

export default useClipboard
