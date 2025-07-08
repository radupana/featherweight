package com.github.radupana.featherweight.viewmodel

import com.github.radupana.featherweight.data.ProgrammeValidationResult
import com.github.radupana.featherweight.service.AIProgrammeResponse

/**
 * Simple singleton to hold the most recently generated programme
 * for passing between generator and preview screens.
 * In a production app, this would be handled through proper navigation arguments
 * or a shared ViewModel with proper lifecycle management.
 */
object GeneratedProgrammeHolder {
    private var _currentResponse: AIProgrammeResponse? = null
    private var _validationResult: ProgrammeValidationResult? = null
    private var _aiRequestId: String? = null
    
    fun setGeneratedProgramme(response: AIProgrammeResponse, aiRequestId: String? = null) {
        _currentResponse = response
        _aiRequestId = aiRequestId
    }
    
    fun getGeneratedProgramme(): AIProgrammeResponse? {
        return _currentResponse
    }
    
    fun getAIRequestId(): String? {
        return _aiRequestId
    }
    
    fun setValidationResult(result: ProgrammeValidationResult) {
        _validationResult = result
    }
    
    fun getValidationResult(): ProgrammeValidationResult? {
        return _validationResult
    }
    
    fun clearGeneratedProgramme() {
        _currentResponse = null
        _validationResult = null
        _aiRequestId = null
    }
}